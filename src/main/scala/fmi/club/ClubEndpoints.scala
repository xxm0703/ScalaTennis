package fmi.club

import cats.data.EitherT
import cats.effect.IO
import fmi.infrastructure.TokenSignatureService
import fmi.user.authentication.AuthenticatedUser
import fmi.user.{UserId, UserRole, UsersDao}
import fmi.*
import sttp.model.StatusCode.{Conflict, Forbidden, NotFound}
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.PartialServerEndpoint

object ClubEndpoints:
  import TennisAppEndpoints.*

  val clubsBaseEndpoint = v1BaseEndpoint.in("clubs")
  val courtsBaseEndpoint = clubsBaseEndpoint.in(path[ClubId].name("club-id")).in("courts")
  val availabilityBaseEndpoint = v1BaseEndpoint.in("availability")

  val getCourtEndpoint = courtsBaseEndpoint
    .in(path[CourtId].name("court-id"))
    .out(jsonBody[Court])
    .errorOut(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
    .get

  val putClubEndpoint =
    clubsBaseEndpoint.secure
      .in(jsonBody[ClubDto])
      .put

  val transferClubEndpoint =
    clubsBaseEndpoint.secure
      .in(path[ClubId].name("club-id"))
      .in("transfer")
      .in(jsonBody[UserId])
      .put

  val putCourtEndpoint =
    courtsBaseEndpoint.secure
      .in(jsonBody[CourtDto])
      .put
