package fmi.reservation

import java.time.Instant
import fmi.utils.CirceUtils
import io.circe.Codec
import io.circe.derivation.Configuration
import sttp.tapir.{Schema, SchemaType}
import fmi.utils.DerivationConfiguration.given Configuration

case class Slot(startTime: Instant, endTime: Instant) derives Codec,
Schema
