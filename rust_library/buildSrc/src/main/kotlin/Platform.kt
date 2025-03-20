object Platform {
  val osName = System.getProperty("os.name")
  val osArch = System.getProperty("os.arch")

  val isMac = osName.startsWith("Mac")
  val isWindows = osName.startsWith("Windows")
  val isLinux = osName.startsWith("Linux")

  val isX86 = setOf("x86", "i386", "amd64", "x86_64").contains(osArch)
  val isArm = listOf("aarch64", "arm").contains(osArch)
}