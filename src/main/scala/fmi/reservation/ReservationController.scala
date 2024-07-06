package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import fmi.{ConflictDescription, ReservationDeletionError, ReservationStatusUpdateError, ResourceNotFound}
import fmi.court.CourtId
import fmi.user.UserRole
import fmi.user.authentication.AuthenticationService
import org.slf4j.{Logger, LoggerFactory}
import sttp.tapir.server.ServerEndpoint

class ReservationController(
  reservationService: ReservationService
)(
  authenticationService: AuthenticationService
):
  import authenticationService.authenticate

  private val getReservation = ReservationEndpoints.retrieveReservationEndpoint
    .authenticate()
    .serverLogic { user => reservationId =>
      println(s"Received request to get reservation with id: $reservationId")
      reservationService
        .getReservationById(reservationId)
        .map {
          case Some(reservation) =>
            println(s"Found reservation: ${reservation.toString}")
            Right(reservation)
          case None =>
            println(s"Reservation with id $reservationId was not found")
            Left(ResourceNotFound(s"Reservation $reservationId was not found"))
        }
    }

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
      println(s"Received request to get reservations for court with id: $courtId")

      reservationService
        .retrieveCourtById(courtId)
        .flatMap {
          case Some(_) =>
            println(s"Retrieved court with id $courtId")
            reservationService.getAllReservationsForCourt(courtId).map(_.asRight)
          case None =>
            IO.pure(Left(ResourceNotFound(s"Court with id $courtId not found")))
            println(s"Court with id $courtId was not found")
        }
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
          user.role match
            case UserRole.Player =>
              if (reservationStatusChangeForm.reservationStatus == ReservationStatus.Approved || reservationStatusChangeForm.reservationStatus == ReservationStatus.Placed) && user.id == reservation.user
              then
                IO.pure(
                  Left(
                    ReservationStatusUpdateError(
                      "User is not authorized to change reservation status to Approved or Placed"
                    )
                  )
                )
              else
                reservationService
                  .updateReservationStatus(reservationStatusChangeForm)
                  .map(_.leftMap(_ => ReservationStatusUpdateError("Reservation status could not be changed")))
            case UserRole.Admin =>
              reservationService
                .updateReservationStatus(reservationStatusChangeForm)
                .map(_.leftMap(_ => ReservationStatusUpdateError("Reservation status could not be changed")))

            case UserRole.Owner =>
              // TODO: check if user is the owner of the club
              reservationService
                .updateReservationStatus(reservationStatusChangeForm)
                .map(_.leftMap(_ => ReservationStatusUpdateError("Reservation status could not be changed")))
        // else IO.pure(Left(ReservationStatusUpdateError("User is not authorized to change reservation status")))
        case None => IO.pure(Left(ReservationStatusUpdateError("No such reservation")))
      }
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    placeReservation,
    getAllReservationsPerCourt,
    deleteReservation,
    updateReservationStatus,
    getReservation
  )
