package fmi.club
import cats.effect.IO
import cats.syntax.all.*
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import fmi.{ConflictDescription, ForbiddenResource, ResourceNotFound}
import sttp.tapir.server.ServerEndpoint

import scala.language.postfixOps

class ClubController(
  courtDao: CourtDao
)(
  authenticationService: AuthenticationService
):
  import authenticationService.*

  val getCourt = ClubEndpoints.getCourtEndpoint.serverLogic: (clubId, courtId) =>
    courtDao
      .retrieveCourt(courtId)
      .map(_.toRight(ResourceNotFound(s"Court $courtId was not found")))

  val putCourt = ClubEndpoints.putCourtEndpoint.authenticateOwner
    .serverLogic { user => (clubId, courtDto) =>
      val court = Court.fromDto(courtDto, clubId)
      courtDao
        .upsertCourtForOwner(user)(clubId, court)
        .map(_.leftMap(ForbiddenResource.apply))
    }

  val putClub = ClubEndpoints.putClubEndpoint.authenticateOwner
    .serverLogic { user => clubDto =>
      val club = Club.fromDto(user.id, clubDto)
      courtDao
        .upsertClub(user, club)
        .map(_.leftMap(ForbiddenResource.apply))
    }
  
  val transferClub = ClubEndpoints.transferClubEndpoint.authenticateOwner
    .serverLogic { user => (clubId, newOwner) =>
      courtDao
        .transferClub(user, clubId, newOwner)
        .map(_.leftMap(ForbiddenResource.apply))
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    getCourt,
    putCourt,
    putClub,
    transferClub
  )
