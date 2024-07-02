package fmi.club

import cats.effect.IO
import cats.syntax.all.*
import doobie.implicits.*
import fmi.infrastructure.db.DoobieDatabase.DbTransactor

class CourtDao(dbTransactor: DbTransactor):
  def retrieveCourt(id: CourtId): IO[Option[Court]] =
    sql"""
      SELECT id, name, description, weight_in_grams
      FROM court
      WHERE id = $id
    """
      .query[Court]
      .option
      .transact(dbTransactor)

  def addCourt(court: Court): IO[Unit] =
    sql"""
      INSERT INTO court as p (id, name, description, weight_in_grams)
      VALUES (${court.id}, ${court.name}, ${court.description}, ${court.weightInGrams})
      ON CONFLICT (id) DO UPDATE SET
      name = EXCLUDED.name,
      description = EXCLUDED.description,
      weight_in_grams = EXCLUDED.weight_in_grams
    """.update.run.void
      .transact(dbTransactor)
