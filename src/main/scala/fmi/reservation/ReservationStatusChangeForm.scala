package fmi.reservation

import io.circe.Codec
import sttp.tapir.Schema

case class ReservationStatusChangeForm(reservationId: ReservationId, reservationStatus: ReservationStatus)
    derives Codec,
      Schema
