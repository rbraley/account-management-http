package accountmanagement.app

import zio.schema.annotation.description
import zio.schema.{ DeriveSchema, Schema }

/*
 * AccountManagementProtocol contains types which are used in the externally facing HTTP interface.
 */
object AccountManagementProtocol {

  sealed trait AccountHttpMessage
  final case class Transaction(
      @description("The account identifier for the transaction")
      `account_id`: String,
      @description("The amount of the transaction (positive for debits, negative for credits)")
      amount: BigDecimal,
      @description("A description of the transaction: e.g. 'groceries'")
      description: String
  ) extends AccountHttpMessage
  object Transaction {
    implicit val schema: Schema[Transaction] = DeriveSchema.gen[Transaction]
  }
  final case class TransactionHistory(
      @description("The unique identifier of the Account")
      `account_id`: String,
      @description("Each of the individual transactions in the Account")
      transactions: List[Transaction]
  ) extends AccountHttpMessage

  object TransactionHistory {
    implicit val schema: Schema[TransactionHistory] = DeriveSchema.gen[TransactionHistory]
  }
  final case class AccountInfo(
      @description("The unique identifier of the Account")
      `account_id`: String,
      @description("The balance of the account in USD")
      balance: BigDecimal,
      @description("The user details, e.g. custom notes on a particular account")
      userDetails: String
  ) extends AccountHttpMessage
  object AccountInfo {
    implicit val schema: Schema[AccountInfo] = DeriveSchema.gen[AccountInfo]
  }

}
