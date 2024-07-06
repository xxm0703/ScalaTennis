package fmi.reservation

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import fmi.court.{Court, CourtId}
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.notification.{NotificationForm, NotificationService, NotificationType}
import fmi.reservation.ReservationStatus.Placed
import fmi.user.{UserId, UserRole}
import fmi.utils.DerivationConfiguration.given
import fmi.{ResourceNotFound, notification}
import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import sttp.tapir.Schema

import java.time.Instant
import java.time.temporal.ChronoUnit

class ReservationService(
  dbTransactor: DbTransactor
)(
  reservationDao: ReservationDao
)(
  notificationService: NotificationService
):

  def getReservationById(reservationId: ReservationId): IO[Option[Reservation]] =
    reservationDao.retrieveReservation(reservationId)

  def placeReservation(userId: UserId, reservationForm: ReservationForm)
    : IO[Either[ReservationSlotAlreadyTaken, Reservation]] = for
    reservationId <- ReservationId.generate
    placingTimestamp <- IO.realTimeInstant

    reservation = Reservation(
      reservationId,
      userId,
      reservationForm.courtId,
      reservationForm.startTime.truncatedTo(ChronoUnit.HOURS),
      placingTimestamp,
      Placed
    )

    maybeReservation <- transactReservation(reservation)

    _ <- maybeReservation.traverse(reservationResult =>
      println(
        s"Will attempt to send a ReservationCreationRequest notification for reservation with id ${reservationResult.reservationId}"
      )

      notificationService
        .createNotification(
          NotificationForm(
            NotificationType.ReservationCreationRequest,
            userId,
            None,
            Some(reservationForm.courtId),
            Some(reservationResult.reservationId),
            Some(userId)
          )
        )
        .map(_ => Right(()))
    )
  yield maybeReservation

  private def transactReservation(
    reservation: Reservation
  ): IO[Either[ReservationSlotAlreadyTaken, Reservation]] =
    (for
      slotTaken <- EitherT.liftF(isSlotAlreadyTaken(reservation.court, reservation.startTime))
      result <-
        if slotTaken then
          EitherT.leftT[IO, Reservation](ReservationSlotAlreadyTaken(reservation.court, reservation.startTime))
        else
          EitherT(reservationDao.createReservation(reservation)).leftMap { case ReservationAlreadyExists(id) =>
            ReservationSlotAlreadyTaken(reservation.court, reservation.startTime)
          }
    yield result).value

  // Each reservation lasts an hour
  private def isSlotAlreadyTaken(courtId: CourtId, startTime: Instant): IO[Boolean] =
    reservationDao.retrieveReservationsForCourt(courtId).map:
      _.contains(startTime)
  def getAllReservationsForCourt(courtId: CourtId): IO[List[Reservation]] =
    reservationDao.retrieveReservationsForCourt(courtId)

  def deleteReservationLogic(reservationId: ReservationId, reservation: Reservation, currentUser: UserId)
    : IO[Either[ResourceNotFound, Unit]] =
    reservationDao.deleteReservation(reservationId).flatMap {
      case Right(_) =>
        println(
          s"Will attempt to send a ReservationDeleted notification for reservation with id ${reservation.reservationId}"
        )

        notificationService
          .createNotification(
            NotificationForm(
              NotificationType.ReservationDeleted,
              currentUser,
              None,
              Some(reservation.court),
              None,
              Some(reservation.user)
            )
          )
          .map(_ =>
            println("Sent Reservation deleted notification")
            Right(())
          )

      case Left(_) => IO.pure(Left(ResourceNotFound("No such reservation was found")))
    }

  def updateReservationStatus(reservationStatusChangeForm: ReservationStatusChangeForm, currentUser: UserId)
    : IO[Either[ReservationNotFound, Reservation]] =
    reservationDao
      .updateReservationStatus(reservationStatusChangeForm.reservationId, reservationStatusChangeForm.reservationStatus)
      .flatMap {
        case Right(res) =>
          if reservationStatusChangeForm.reservationStatus == ReservationStatus.Cancelled || reservationStatusChangeForm.reservationStatus == ReservationStatus.Approved
          then
            println(
              s"Will attempt to send a ${reservationStatusChangeForm.reservationStatus} notification for reservation with id ${res.reservationId}"
            )
            val notificationType =
              if reservationStatusChangeForm.reservationStatus == ReservationStatus.Cancelled then
                NotificationType.ReservationCancelled
              else NotificationType.ReservationApproved

            notificationService
              .createNotification(
                NotificationForm(
                  notificationType,
                  currentUser,
                  None,
                  Some(res.court),
                  None,
                  Some(res.user)
                )
              )
              .map(_ =>
                println(s"Sent ${notificationType} notification")
                Right(res)
              )
          else IO.pure(Right(res))

        case Left(_) => IO.pure(Left(ReservationNotFound(reservationStatusChangeForm.reservationId)))
      }
  def retrieveCourtById(courtId: CourtId): IO[Option[Court]] = reservationDao.retrieveCourtById(courtId)

  def retrieveCourtForReservation(reservationId: ReservationId): IO[Option[Court]] =
    reservationDao.retrieveCourtForReservation(reservationId)

  def retrieveCourtOwnerForReservation(reservationId: ReservationId): IO[Option[UserId]] =
    reservationDao.retrieveCourtOwnerForReservation(reservationId)

  def isReservationStatusUpdateValidForPlayerUser(
    userId: UserId,
    reservationStatus: ReservationStatus,
    reservation: Reservation
  ): IO[Boolean] = IO.pure(userId == reservation.user && reservationStatus == ReservationStatus.Cancelled)

  def getReservedSlotsForCourt(courtId: CourtId): IO[List[Slot]] =
    reservationDao
      .retrieveReservedSlotsForCourt(courtId)
      .map:
        _.map: startTime =>
          Slot(startTime, startTime.plus(1, ChronoUnit.HOURS))

sealed trait ReservationError derives ConfiguredCodec, Schema
case class ReservationAlreadyExists(reservation: ReservationId) extends ReservationError
case class ReservationSlotAlreadyTaken(court: CourtId, startTime: Instant) extends ReservationError
case class ReservationNotFound(reservationId: ReservationId) extends ReservationError
