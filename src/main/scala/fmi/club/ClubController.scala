package fmi.club
import cats.effect.IO
import cats.syntax.all.*
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import fmi.{ConflictDescription, ForbiddenResource, ResourceNotFound}
import sttp.tapir.server.ServerEndpoint

import scala.language.postfixOps

class ClubController(
  clubDao: ClubDao
)(
  authenticationService: AuthenticationService
):
  import authenticationService.*

  val putClub = ClubEndpoints.putClubEndpoint.authenticateOwner
    .serverLogic { user => clubDto =>
      val club = Club.fromDto(user.id, clubDto)
      clubDao
        .upsertClub(user, club)
        .map(_.leftMap(ForbiddenResource.apply))
    }

  val transferClub = ClubEndpoints.transferClubEndpoint.authenticateOwner
    .serverLogic { user => (clubId, newOwner) =>
      clubDao
        .transferClub(user, clubId, newOwner)
        .map(_.leftMap(ForbiddenResource.apply))
    }

  val getClubs = ClubEndpoints.getClubsEndpoint.authenticateOwner
    .serverLogicSuccess: user =>
      _ => clubDao.getOwnedClubs(user.id)

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    putClub,
    transferClub,
    getClubs
  )
