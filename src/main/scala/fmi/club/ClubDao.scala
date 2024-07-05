package fmi.club

import cats.effect.IO
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.UserId
import fmi.user.authentication.AuthenticatedUser
import fmi.{AuthenticationError, ForbiddenResource}

class ClubDao(dbTransactor: DbTransactor):
  import fmi.user.UserDbGivens.given


  def getOwnedClubs(id: UserId): IO[List[Club]] =
    sql"""
      SELECT id, name, description, owner
      FROM club
      WHERE club.owner = $id
    """
      .query[Club]
      .to[List]
      .transact(dbTransactor)
  
  def isUserOwner(user: AuthenticatedUser, id: ClubId): ConnectionIO[Option[Int]] =
    sql"""
        SELECT 1
        FROM club
        WHERE ${user.role} = "admin" OR (club.owner = ${user.id} AND club.id = $id)
    """
      .query[Int]
      .option
  
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
