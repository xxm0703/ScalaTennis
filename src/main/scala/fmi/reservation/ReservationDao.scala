package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import doobie.postgres.sqlstate
import fmi.court.{Court, CourtId}
import fmi.user.{User, UserId}

import java.time.Instant

class ReservationDao(dbTransactor: DbTransactor):

  def retrieveReservation(id: ReservationId): IO[Option[Reservation]] =
    sql"""
    SELECT *
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
            SELECT *
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

  def deleteReservation(id: ReservationId): IO[Either[ReservationNotFound, Unit]] =
    sql"""
         DELETE FROM reservation
         WHERE id = $id
       """.update.run
      .transact(dbTransactor)
      .flatMap {
        case 0 => IO.pure(Left(ReservationNotFound(id)))
        case _ => IO.pure(Right(()))
      }

  def updateReservationStatus(id: ReservationId, newStatus: ReservationStatus)
    : IO[Either[ReservationNotFound, Reservation]] =
    val updateQuery =
      sql"""
      UPDATE reservation
      SET reservation_status = $newStatus
      WHERE id = $id
    """.update.run

    val retrieveQuery = sql"""
      SELECT *
      FROM reservation
      WHERE id = $id
    """.query[Reservation].option

    updateQuery.transact(dbTransactor).flatMap {
      case 0 => IO.pure(Left(ReservationNotFound(id)))
      case _ =>
        retrieveQuery.transact(dbTransactor).map {
          case Some(updatedReservation) => Right(updatedReservation)
          case None =>
            Left(
              ReservationNotFound(id)
            ) // This case is unlikely but handles the case where the reservation is not found after update.
        }
    }

  def retrieveCourtById(courtId: CourtId): IO[Option[Court]] =
    sql"""
      SELECT *
      FROM court
      WHERE id = $courtId
    """
      .query[Court]
      .option
      .transact(dbTransactor)

  def retrieveCourtForReservation(reservationId: ReservationId): IO[Option[Court]] =
    sql"""
      SELECT c.id AS court_id, c.name AS court_name, c.surface AS court_surface, c.club_id AS club_id,
             cl.name AS club_name, cl.description AS club_description, cl.owner AS club_owner
      FROM reservation r
      JOIN court c ON r.court_id = c.id
      JOIN club cl ON c.club_id = cl.id
      WHERE r.id = $reservationId
      """
      .query[Court]
      .option
      .transact(dbTransactor)

  def retrieveCourtOwnerForReservation(reservationId: ReservationId): IO[Option[UserId]] =
    sql"""
         SELECT
             cl.owner AS club_owner
         FROM
             reservation r
         JOIN
             court c ON r.court_id = c.id
         JOIN
             club cl ON c.club_id = cl.id
         WHERE
             r.id = $reservationId
         """
      .query[UserId]
      .option
      .transact(dbTransactor)
