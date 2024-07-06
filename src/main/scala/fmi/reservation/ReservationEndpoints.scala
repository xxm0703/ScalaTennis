package fmi.reservation

import fmi.court.CourtId
import fmi.{
  AuthenticationError,
  ConflictDescription,
  ReservationDeletionError,
  ReservationStatusUpdateError,
  ReservationCreationError,
  ResourceNotFound,
  TennisAppEndpoints
}
import sttp.model.StatusCode.{BadRequest, Conflict, NoContent, NotFound}
import sttp.tapir.{oneOfVariant, statusCode, *}
import sttp.tapir.json.circe.*

object ReservationEndpoints:
  import TennisAppEndpoints.*

  private val reservationsBaseEndpoint = v1BaseEndpoint.in("reservations")

  val retrieveReservationEndpoint
    : Endpoint[String, ReservationId, ResourceNotFound | AuthenticationError, Reservation, Any] =
    reservationsBaseEndpoint.secure
      .in(path[ReservationId].name("reservation-id"))
      .out(jsonBody[Reservation])
      .errorOutVariantPrepend[AuthenticationError | ResourceNotFound](
        oneOfVariant(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
      )
      .get

  val placeReservationEndpoint: Endpoint[
    String,
    ReservationForm,
    AuthenticationError | ReservationCreationError,
    Reservation,
    Any
  ] =
    reservationsBaseEndpoint.secure
      .in(jsonBody[ReservationForm])
      .out(jsonBody[Reservation])
      .errorOutVariantPrepend[AuthenticationError | ReservationCreationError](
        oneOfVariant(
          statusCode(Conflict).and(jsonBody[ReservationCreationError])
        )
      )
      .post

  val getAllReservationsForCourtEndpoint
    : Endpoint[String, CourtId, ResourceNotFound | AuthenticationError, List[Reservation], Any] =
    reservationsBaseEndpoint.secure
      .in("court")
      .in(path[CourtId].name("court-id"))
      .out(jsonBody[List[Reservation]])
      .errorOutVariantPrepend[AuthenticationError | ResourceNotFound](
        oneOfVariant(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
      )
      .get

  val deleteReservationEndpoint
    : Endpoint[String, ReservationId, AuthenticationError | ReservationDeletionError, Unit, Any] =
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

  val getReservedSlotsForCourtEndpoint
    : Endpoint[String, CourtId, ResourceNotFound | AuthenticationError, List[Slot], Any] =
    reservationsBaseEndpoint.secure
      .in("court")
      .in(path[CourtId]("court-id") / "reserved-slots")
      .out(jsonBody[List[Slot]])
      .errorOutVariantPrepend[AuthenticationError | ResourceNotFound](
        oneOfVariant(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
      )
      .get
