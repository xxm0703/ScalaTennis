package fmi.court

import cats.effect.IO
import cats.syntax.all.*
import fmi.court.CourtDao
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import fmi.{ConflictDescription, ForbiddenResource, ResourceNotFound}
import sttp.tapir.server.ServerEndpoint

import scala.language.postfixOps

class CourtController(
  courtDao: CourtDao
)(
  authenticationService: AuthenticationService
):
  import authenticationService.*

  val getCourt = CourtEndpoints.getCourtEndpoint.serverLogic: (clubId, courtId) =>
    courtDao
      .retrieveCourt(courtId)
      .map(_.toRight(ResourceNotFound(s"Court $courtId was not found")))

  val putCourt = CourtEndpoints.putCourtEndpoint.authenticateOwner
    .serverLogic { user => (clubId, courtDto) =>
      val court = Court.fromDto(courtDto, clubId)
      courtDao
        .upsertCourtForOwner(user)(clubId, court)
        .map(_.leftMap(ForbiddenResource.apply))
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    getCourt,
    putCourt
  )
