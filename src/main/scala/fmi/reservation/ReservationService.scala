package fmi.reservation

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import doobie.implicits.*
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.club.{
  ClubAdjustment,
  CourtAvailabilityDao,
  CourtId,
  NotEnoughAvailabilityAvailable,
  NotEnoughAvailabilityAvailableException
}
import fmi.reservation.ReservationForm
import fmi.user.UserId
import fmi.utils.DerivationConfiguration.given
import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import sttp.tapir.Schema

import java.security.KeyStore.TrustedCertificateEntry
import java.time.Instant

class ReservationService(dbTransactor: DbTransactor)(reservationDao: ReservationDao):
  def placeReservation(userId: UserId, reservationForm: ReservationForm)
    : IO[Either[ReservationSlotAlreadyTaken, Reservation]] = for
    reservationId <- ReservationId.generate
    placingTimestamp <- IO.realTimeInstant

    reservation = Reservation(
      reservationId,
      userId,
      reservationForm.courtId,
      reservationForm.startTime,
      placingTimestamp
    )

    maybeReservation <- transactReservation(reservation)
  yield maybeReservation

  private def isSlotAlreadyTaken(courtId: CourtId, startTime: Instant): IO[Boolean] =
    reservationDao.retrieveReservationAtSlot(courtId, startTime).map {
      case Some(_) => true
      case None => false
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

sealed trait ReservationError derives ConfiguredCodec, Schema
case class ReservationAlreadyExists(reservation: ReservationId) extends ReservationError
case class ReservationSlotAlreadyTaken(court: CourtId, startTime: Instant) extends ReservationError
