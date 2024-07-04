package fmi.club

import io.circe.Codec
import sttp.tapir.Schema

case class CourtAvailability(court: CourtId, quantity: Int) derives Codec, Schema

case class CourtAvailabilityAdjustment(court: CourtId, adjustmentQuantity: Int) derives Codec, Schema
case class ClubAdjustment(adjustments: List[CourtAvailabilityAdjustment]) derives Codec, Schema
