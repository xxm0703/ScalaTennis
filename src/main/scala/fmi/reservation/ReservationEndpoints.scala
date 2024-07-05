package fmi.reservation

import fmi.club.CourtId
import fmi.{AuthenticationError, ConflictDescription, ResourceNotFound, TennisAppEndpoints}
import sttp.model.StatusCode.{Conflict, NotFound}
import sttp.tapir.*
import sttp.tapir.json.circe.*

object ReservationEndpoints:
  import TennisAppEndpoints.*

  private val reservationsBaseEndpoint = v1BaseEndpoint.in("reservations")

  val placeReservationEndpoint
    : Endpoint[String, ReservationForm, AuthenticationError | ConflictDescription, Reservation, Any] =
    reservationsBaseEndpoint.secure
      .in(jsonBody[ReservationForm])
      .out(jsonBody[Reservation])
      .errorOutVariantPrepend[AuthenticationError | ConflictDescription](
        oneOfVariant(statusCode(Conflict).and(jsonBody[ConflictDescription]))
      )
      .post

  val getAllReservationsForCourtEndpoint
    : Endpoint[String, CourtId, ResourceNotFound | AuthenticationError, List[Reservation], Any] =
    reservationsBaseEndpoint.secure
      .in(path[CourtId].name("court-id"))
      .out(jsonBody[List[Reservation]])
      .errorOutVariantPrepend[AuthenticationError | ResourceNotFound](
        oneOfVariant(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
      )
      .get
