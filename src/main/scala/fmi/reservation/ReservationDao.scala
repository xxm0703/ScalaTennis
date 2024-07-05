package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import doobie.postgres.sqlstate
import fmi.club.CourtId

import java.time.Instant

class ReservationDao(dbTransactor: DbTransactor):

  def retrieveReservation(id: ReservationId): IO[Option[Reservation]] =
    sql"""
    SELECT user_id, court_id, start_time, placing_timestamp, reservation_status
    FROM reservation
    WHERE id = $id
    """
      .query[Reservation]
      .option
      .transact(dbTransactor)

  def createReservation(reservation: Reservation): IO[Either[ReservationAlreadyExists, Reservation]] =
    sql"""
         INSERT INTO reservation (id, user_id, court_id, start_time, placing_timestamp, reservation_status)
         VALUES (${reservation.reservationId}, ${reservation.user}, ${reservation.court}, ${reservation.startTime}, ${reservation.placingTimestamp}, "placed")
       """.update.run
      .as(reservation)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        ReservationAlreadyExists(reservation.reservationId)
      }
      .transact(dbTransactor)

  def retrieveReservationAtSlot(court: CourtId, startTime: Instant): IO[Option[Reservation]] =
    sql"""
            SELECT id, user_id, court_id, start_time, placing_timestamp, reservation_status
            FROM reservation
            WHERE court_id = $court AND start_time = $startTime 
          """
      .query[Reservation]
      .option
      .transact(dbTransactor)

  def retrieveReservationsForCourt(court: CourtId): IO[List[Reservation]] =
    sql"""
             SELECT *
             FROM reservation
             WHERE court_id = $court
           """
      .query[Reservation]
      .to[List]
      .transact(dbTransactor)

  def deleteReservation(id: ReservationId): IO[Either[ReservationNotFound, Unit]] = {
    sql"""
         DELETE FROM reservation
         WHERE id = $id
       """.update.run
      .transact(dbTransactor)
      .flatMap {
        case 0 => IO.pure(Left(ReservationNotFound(id)))
        case _ => IO.pure(Right(()))
      }
  }
