package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import fmi.{
  ConflictDescription,
  OperationNotAllowed,
  ReservationDeletionError,
  ReservationStatusUpdateError,
  ResourceNotFound
}
import fmi.club.CourtId
import fmi.user.UserRole
import fmi.user.authentication.AuthenticationService
import sttp.tapir.server.ServerEndpoint

class ReservationController(reservationService: ReservationService)(authenticationService: AuthenticationService):
  import authenticationService.authenticate

  private val placeReservation = ReservationEndpoints.placeReservationEndpoint
    .authenticate()
    .serverLogic { user => reservationForm =>
      reservationService
        .placeReservation(user.id, reservationForm)
        .map(_.leftMap(_ => ConflictDescription("Slot is already taken")))
    }

  private val getAllReservationsPerCourt = ReservationEndpoints.getAllReservationsForCourtEndpoint
    .authenticate()
    .serverLogic { user => courtId =>
      reservationService
        .getAllReservationsForCourt(courtId)
        .map(_.asRight)
    }
  
  private val deleteReservation = ReservationEndpoints.deleteReservationEndpoint
    .authenticate()
    .serverLogic { user => reservationId =>
      reservationService.getReservationById(reservationId).flatMap {
        case Some(reservation) =>
          if user.role == UserRole.Admin || user.id == reservation.user || user.role == UserRole.Owner then
            reservationService
              .deleteReservationLogic(reservationId)
              .map(_.leftMap(_ => ReservationDeletionError("Reservation could not be deleted")))
          else IO.pure(Left(ReservationDeletionError("User is not authorized to delete reservations")))
        case None => IO.pure(Left(ReservationDeletionError("No such reservation")))
      }
    }
  
  private val updateReservationStatus = ReservationEndpoints.changeReservationStatusEndpoint
    .authenticate()
    .serverLogic { user => reservationStatusChangeForm =>
      reservationService.getReservationById(reservationStatusChangeForm.reservationId).flatMap {
        case Some(reservation) =>
          if user.role == UserRole.Admin || user.id == reservation.user then
            reservationService
              .updateReservationStatus(reservationStatusChangeForm)
              .map(_.leftMap(_ => ReservationStatusUpdateError("Reservation status could npt be changed")))
          else IO.pure(Left(ReservationStatusUpdateError("User is not authorized to change reservation status")))
        case None => IO.pure(Left(ReservationStatusUpdateError("No such reservation")))
      }
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    placeReservation,
    getAllReservationsPerCourt,
    deleteReservation,
    updateReservationStatus
  )
