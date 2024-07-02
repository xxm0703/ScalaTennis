package fmi.club

import fmi.{AuthenticationError, ConflictDescription, ResourceNotFound, ShoppingAppEndpoints}
import sttp.model.StatusCode.{Conflict, NotFound}
import sttp.tapir.*
import sttp.tapir.json.circe.*

object ClubEndpoints:
  import ShoppingAppEndpoints.*

  val courtsBaseEndpoint = v1BaseEndpoint.in("courts")
  val availabilityBaseEndpoint = v1BaseEndpoint.in("availability")

  val getCourtEndpoint = courtsBaseEndpoint
    .in(path[CourtId].name("court-id"))
    .out(jsonBody[Court])
    .errorOut(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
    .get

  val putCourtEndpoint =
    courtsBaseEndpoint.secure
      .in(jsonBody[Court])
      .post

  val getAllAvailabilityEndpoint = availabilityBaseEndpoint
    .out(jsonBody[List[CourtAvailability]])
    .get

  val adjustAvailabilityEndpoint: Endpoint[String, ClubAdjustment, AuthenticationError | ConflictDescription, Unit, Any] =
    availabilityBaseEndpoint.secure
      .in(jsonBody[ClubAdjustment])
      .errorOutVariantPrepend[AuthenticationError | ConflictDescription](
        oneOfVariant(statusCode(Conflict).and(jsonBody[ConflictDescription]))
      )
      .post
