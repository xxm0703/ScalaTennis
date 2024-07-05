package fmi.reservation

import fmi.club.CourtId
import fmi.{
  AuthenticationError,
  ConflictDescription,
  ReservationDeletionError,
  ReservationStatusUpdateError,
  ResourceNotFound,
  TennisAppEndpoints
}
import sttp.model.StatusCode.{BadRequest, Conflict, Forbidden, NoContent, NotFound, Unauthorized}
import sttp.tapir.{oneOfVariant, *}
import sttp.tapir.json.circe.*
case class ErrorResponse(message: String)

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

  val deleteReservationEndpoint
    : Endpoint[String, ReservationId, (AuthenticationError | ReservationDeletionError), Unit, Any] =
    reservationsBaseEndpoint.secure
      .in(path[ReservationId].name("reservation-id"))
      .out(statusCode(NoContent))
      .errorOutVariantPrepend[AuthenticationError | ReservationDeletionError](
        oneOfVariant(
          statusCode(BadRequest).and(jsonBody[ReservationDeletionError].description("No such reservation was found"))
        )
      )
      .delete

  val changeReservationStatusEndpoint: Endpoint[
    String,
    ReservationStatusChangeForm,
    AuthenticationError | ReservationStatusUpdateError,
    Reservation,
    Any
  ] =
    reservationsBaseEndpoint.secure
      .in(jsonBody[ReservationStatusChangeForm])
      .out(jsonBody[Reservation])
      .errorOutVariantPrepend[AuthenticationError | ReservationStatusUpdateError](
        oneOfVariant(
          statusCode(BadRequest)
            .and(jsonBody[ReservationStatusUpdateError].description("Reservation status could not be changed."))
        )
      )
      .put
