package fmi.club

import cats.effect.IO
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
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

  def getOwnedClubs(id: UserId): IO[List[Club]] =
    sql"""
      SELECT id, name, description, owner
      FROM club
      WHERE club.owner = $id
    """
      .query[Club]
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

  def upsertClub(user: AuthenticatedUser, club: Club): IO[Either[String, Unit]] =
    checkOwnershipAndExistence(user, club.id)
      .flatMap {
        if _ then insertOrUpdateClub(club) >> ().asRight.pure[ConnectionIO]
        else Left("You do not have permission to update this club").pure[ConnectionIO]
      }
      .transact(dbTransactor)

  private def checkOwnershipAndExistence(user: AuthenticatedUser, clubId: ClubId): ConnectionIO[Boolean] =
    sql"""
      SELECT NOT EXISTS (
        SELECT 1 FROM club WHERE ${user.role} = "admin" OR (id = $clubId AND owner = ${user.id})
      ) XOR EXISTS (
        SELECT 1 FROM club WHERE id = $clubId
      )
    """.query[Boolean].unique

  private def insertOrUpdateClub(club: Club): ConnectionIO[Int] =
    sql"""
      INSERT INTO club (id, name, description, owner)
      VALUES (${club.id}, ${club.name}, ${club.description}, ${club.owner})
      ON DUPLICATE KEY UPDATE
      name = VALUES(name),
      description = VALUES(description),
      owner = VALUES(owner)
    """.update.run

  def transferClub(user: AuthenticatedUser, clubId: ClubId, newOwner: UserId): IO[Either[String, Unit]] =
    (for
      existsNewOwner <- userExists(newOwner)
      transferable <- checkOwnershipAndExistence(user, clubId)
      result <-
        if !transferable then Left("You do not have permission to transfer this club").pure[ConnectionIO]
        if existsNewOwner.isEmpty then Left("New owner does not exist").pure[ConnectionIO]
        else transferOwnership(clubId, newOwner) >> ().asRight.pure[ConnectionIO]
    yield result).transact(dbTransactor)

  private def userExists(newOwner: UserId) =
    sql"SELECT 1 FROM user WHERE email = $newOwner".query[Int].option

  private def transferOwnership(clubId: ClubId, newOwner: UserId): ConnectionIO[Int] =
    sql"""
      UPDATE club
      SET owner = $newOwner
      WHERE id = $clubId
    """.update.run
