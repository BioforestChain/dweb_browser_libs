object Platform {
  val osName = System.getProperty("os.name")

  val isMac = osName.startsWith("Mac")
  val isWindows = osName.startsWith("Windows")
  val isLinux = osName.startsWith("Linux")
}