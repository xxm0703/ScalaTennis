package fmi.reservation

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import fmi.ResourceNotFound
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.club.CourtId
import fmi.reservation.ReservationStatus.Placed
import fmi.user.UserId
import fmi.utils.DerivationConfiguration.given
import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import sttp.tapir.Schema

import java.time.Instant
import java.time.temporal.ChronoUnit

class ReservationService(dbTransactor: DbTransactor)(reservationDao: ReservationDao):
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
      reservationForm.startTime,
      placingTimestamp,
      Placed
    )

    maybeReservation <- transactReservation(reservation)
  yield maybeReservation

  // Each reservation lasts an hour
  private def isSlotAlreadyTaken(courtId: CourtId, startTime: Instant): IO[Boolean] =
    val endTime = startTime.plus(1, ChronoUnit.HOURS)
    reservationDao.retrieveReservationsForCourt(courtId).map { reservations =>
      reservations.exists { reservation =>
        val existingStart = reservation.startTime
        val existingEnd = reservation.startTime.plus(1, ChronoUnit.HOURS)
        startTime.isBefore(existingEnd) && endTime.isAfter(existingStart)
      }
    }
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

  def getAllReservationsForCourt(courtId: CourtId): IO[List[Reservation]] =
    reservationDao.retrieveReservationsForCourt(courtId)

  def deleteReservationLogic(reservationId: ReservationId): IO[Either[ResourceNotFound, Unit]] =
    reservationDao.deleteReservation(reservationId).flatMap {
      case Right(_) => IO.pure(Right(()))
      case Left(_) => IO.pure(Left(ResourceNotFound("No such reservation was found")))
    }

  def updateReservationStatus(reservationStatusChangeForm: ReservationStatusChangeForm)
    : IO[Either[ReservationNotFound, Reservation]] =
    reservationDao
      .updateReservationStatus(reservationStatusChangeForm.reservationId, reservationStatusChangeForm.reservationStatus)
      .flatMap {
        case Right(res) => IO.pure(Right(res))
        case Left(_) => IO.pure(Left(ReservationNotFound(reservationStatusChangeForm.reservationId)))
      }

sealed trait ReservationError derives ConfiguredCodec, Schema
case class ReservationAlreadyExists(reservation: ReservationId) extends ReservationError
case class ReservationSlotAlreadyTaken(court: CourtId, startTime: Instant) extends ReservationError
case class ReservationNotFound(reservationId: ReservationId) extends ReservationError
