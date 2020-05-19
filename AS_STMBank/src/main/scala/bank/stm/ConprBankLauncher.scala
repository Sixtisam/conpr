package bank.stm;

import bank.gui.ClientFX;

/**
	* Launcher for the stm bank.
	*/
object ConprBankLauncher {
	def main(args: Array[String]): Unit = {
		ClientFX.main(Array(classOf[STMBankDriver].getName()))
	}
}
