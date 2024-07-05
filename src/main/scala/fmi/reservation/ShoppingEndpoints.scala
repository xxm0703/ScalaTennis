package fmi.reservation

import fmi.{AuthenticationError, ConflictDescription, ResourceNotFound, TennisAppEndpoints}
import sttp.model.StatusCode.{Conflict, NotFound}
import sttp.tapir.*
import sttp.tapir.json.circe.*

object TennisEndpoints:
  import TennisAppEndpoints.*

  val ordersBaseEndpoint = v1BaseEndpoint.in("orders")

  val placeOrderEndpoint = ordersBaseEndpoint.secure
    .in(jsonBody[TennisCart])
    .out(jsonBody[Order])
    .errorOutVariantPrepend[AuthenticationError | ConflictDescription](
      oneOfVariant(statusCode(Conflict).and(jsonBody[ConflictDescription]))
    )
