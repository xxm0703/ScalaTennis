package fmi.court

import cats.data.EitherT
import cats.effect.IO
import fmi.*
import fmi.club.*
import fmi.club.ClubEndpoints.clubsBaseEndpoint
import fmi.infrastructure.TokenSignatureService
import fmi.user.authentication.AuthenticatedUser
import fmi.user.{UserId, UserRole, UsersDao}
import sttp.model.StatusCode.{Conflict, Forbidden, NotFound}
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.PartialServerEndpoint

object CourtEndpoints:
  import TennisAppEndpoints.*

  val courtsBaseEndpoint = clubsBaseEndpoint.in(path[ClubId].name("club-id")).in("courts")

  val getCourtEndpoint = courtsBaseEndpoint
    .in(path[CourtId].name("court-id"))
    .out(jsonBody[Court])
    .errorOut(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
    .get
  
  val putCourtEndpoint =
    courtsBaseEndpoint.secure
      .in(jsonBody[CourtDto])
      .put
