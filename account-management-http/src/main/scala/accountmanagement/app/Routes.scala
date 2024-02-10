package accountmanagement.app

import accountmanagement.app.HttpApp.{
  createTransaction,
  createTransactionHandler,
  getAccount,
  getAccountHandler,
  getTransactionHistory,
  getTransactionHistoryHandler
}
import zio.http.Handler

object Routes {
  val getAccountRoute =
    getAccount.implement {
      Handler.fromFunctionZIO(getAccountHandler)
    }

  val createTransactionRoute =
    createTransaction.implement {
      Handler.fromFunctionZIO(createTransactionHandler)
    }

  val getTransactionHistoryRoute =
    getTransactionHistory.implement {
      Handler.fromFunctionZIO(getTransactionHistoryHandler)
    }
}
