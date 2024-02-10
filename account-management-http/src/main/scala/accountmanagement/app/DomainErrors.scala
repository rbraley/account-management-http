package accountmanagement.app

import zio.schema.{ DeriveSchema, Schema }

object DomainErrors {
  case class InsufficientFundsError() extends Throwable
  object InsufficientFundsError {
    implicit val schema: Schema[InsufficientFundsError] = DeriveSchema.gen[InsufficientFundsError]
  }

  case class AccountNotFound()
  object AccountNotFound {
    implicit val schema: Schema[AccountNotFound] = DeriveSchema.gen[AccountNotFound]
  }

}
