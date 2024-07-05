package fmi.court

import cats.effect.IO
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
import fmi.club.{Club, ClubId}
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.UserId
import fmi.user.authentication.AuthenticatedUser
import fmi.{AuthenticationError, ForbiddenResource}

class CourtDao(dbTransactor: DbTransactor):
  import fmi.user.UserDbGivens.given

  def retrieveCourt(id: CourtId): IO[Option[Court]] =
    sql"""
      SELECT id, name, surface, club_id
      FROM court
      WHERE id = $id
    """
      .query[Court]
      .option
      .transact(dbTransactor)

  def getOwnedCourts(id: UserId): IO[List[Court]] =
    sql"""
      SELECT court.id, court.name, surface, club_id
      FROM club
      JOIN court ON club.id = court.club_id
      WHERE club.owner = $id
    """
      .query[Court]
      .to[List]
      .transact(dbTransactor)

  def upsertCourtForOwner(user: AuthenticatedUser)(clubId: ClubId, court: Court): IO[Either[String, Unit]] =
    (for

      isOwner <- isUserOwner(user, clubId)
      result <- isOwner match
        case Some(_) => addCourt(court) >> ().asRight.pure[ConnectionIO]
        case None => Left("User is not the owner of this club").pure[ConnectionIO]
    yield result).transact(dbTransactor)

  def isUserOwner(user: AuthenticatedUser, id: ClubId): ConnectionIO[Option[Int]] =
    sql"""
        SELECT 1
        FROM club
        WHERE ${user.role} = "admin" OR (club.owner = ${user.id} AND club.id = $id)
    """
      .query[Int]
      .option

  private def addCourt(court: Court): ConnectionIO[Int] =
    sql"""
      INSERT INTO court (id, name, surface, club_id)
      VALUES (${court.id}, ${court.name}, ${court.surface}, ${court.clubId})
      ON DUPLICATE KEY UPDATE
      name = VALUES(name),
      surface = VALUES(surface),
      club_id = VALUES(club_id);
    """.update.run
