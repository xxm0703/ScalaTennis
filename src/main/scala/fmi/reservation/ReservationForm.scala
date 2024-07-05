package fmi.reservation

import fmi.club.CourtId
import io.circe.Codec
import sttp.tapir.Schema

import java.time.Instant

case class ReservationForm(courtId: CourtId, startTime: Instant) derives Codec, Schema
