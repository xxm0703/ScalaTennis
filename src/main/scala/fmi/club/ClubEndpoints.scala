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
  
  val putClubEndpoint =
    clubsBaseEndpoint.secure
      .in(jsonBody[ClubDto])
      .put

  val getClubsEndpoint =
    clubsBaseEndpoint.secure
      .out(jsonBody[List[Club]])
      .get
  
  val transferClubEndpoint =
    clubsBaseEndpoint.secure
      .in(path[ClubId].name("club-id"))
      .in("transfer")
      .in(jsonBody[UserId])
      .put
