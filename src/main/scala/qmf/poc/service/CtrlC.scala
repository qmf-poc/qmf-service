package qmf.poc.service

import zio.ZIO

val ctrlC = ZIO.async { cb =>
  System.out.println("Install Ctrl-C hook")
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      System.out.println("Ctrl-C detected")
      cb(ZIO.unit)
    }
  })
}
