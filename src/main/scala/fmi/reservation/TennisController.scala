package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import fmi.ConflictDescription
import fmi.club.CourtId
import fmi.user.UserId
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import fmi.utils.CirceUtils
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.http4s.AuthedRoutes
import org.http4s.dsl.io.*
import sttp.tapir.server.ServerEndpoint

class TennisController(orderService: OrderService)(authenticationService: AuthenticationService):
  import authenticationService.authenticate

  val placeReservation = TennisEndpoints.placeOrderEndpoint
    .authenticate()
    .serverLogic { user => tennisCart =>
      orderService
        .placeOrder(user.id, tennisCart)
        .map(_.leftMap(_ => ConflictDescription("Not enough availability available")))
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(placeReservation)
