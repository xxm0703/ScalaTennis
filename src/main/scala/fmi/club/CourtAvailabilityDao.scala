//package fmi.club
//
//import cats.effect.IO
//import cats.syntax.all.*
//import doobie.*
//import doobie.implicits.*
//import fmi.infrastructure.db.DoobieDatabase.DbTransactor
//import fmi.utils.DerivationConfiguration.given
//import io.circe.derivation.ConfiguredCodec
//import sttp.tapir.Schema
//
//class CourtAvailabilityDao(dbTransactor: DbTransactor):
//  def retrieveAllAvailableAvailability: IO[List[CourtAvailability]] =
//    sql"""
//      SELECT court_id, quantity
//      FROM court_availability
//      WHERE quantity > 0
//    """
//      .query[CourtAvailability]
//      .to[List]
//      .transact(dbTransactor)
//
//  def retrieveAvailabilityForCourt(courtId: CourtId): IO[Option[CourtAvailability]] =
//    sql"""
//      SELECT court_id, quantity
//      FROM court_availability
//      WHERE court_id = $courtId
//    """
//      .query[CourtAvailability]
//      .option
//      .transact(dbTransactor)
//
//  def applyClubAdjustment(clubAdjustment: ClubAdjustment): IO[AdjustmentResult] =
//    applyClubAdjustmentAction(clubAdjustment)
//      .transact(dbTransactor)
//      .as(SuccessfulAdjustment: AdjustmentResult)
//      .recover { case NotEnoughAvailabilityAvailableException =>
//        NotEnoughAvailabilityAvailable
//      }
//
//  def applyClubAdjustmentAction(clubAdjustment: ClubAdjustment): ConnectionIO[Unit] =
//    // TODO: Replace by a single query
//    clubAdjustment.adjustments
//      // Sorting allows us to avoid deadlock
//      .sortBy(_.court.id)
//      .map(adjustAvailabilityForCourt)
//      .sequence
//      .void
//
//  private def adjustAvailabilityForCourt(adjustment: CourtAvailabilityAdjustment): ConnectionIO[Unit] =
//    val addQuery = sql"""
//      INSERT INTO court_availability as ps (court_id, quantity)
//      VALUES (${adjustment.court}, ${adjustment.adjustmentQuantity})
//      ON CONFLICT (court_id) DO UPDATE SET
//      quantity = ps.quantity + EXCLUDED.quantity
//    """
//    val substractQuery = sql"""
//      UPDATE court_availability
//      SET quantity = quantity + ${adjustment.adjustmentQuantity}
//      WHERE court_id = ${adjustment.court} AND quantity + ${adjustment.adjustmentQuantity} >= 0
//    """
//
//    val query = if adjustment.adjustmentQuantity >= 0 then addQuery else substractQuery
//
//    query.update.run
//      .map(updatedRows => updatedRows == 1)
//      .ifM[Unit](().pure[ConnectionIO], NotEnoughAvailabilityAvailableException.raiseError[ConnectionIO, Unit])
//
//case object NotEnoughAvailabilityAvailableException extends Exception
//
//sealed trait AdjustmentResult derives ConfiguredCodec, Schema
//case object SuccessfulAdjustment extends AdjustmentResult
//case object NotEnoughAvailabilityAvailable extends AdjustmentResult
