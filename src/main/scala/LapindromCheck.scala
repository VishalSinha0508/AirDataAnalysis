import scala.io.StdIn.readLine

object LapindromCheck {
  // Main method
  def main(args: Array[String]): Unit = {
    var i = ""

    // Looping until the user types 'exit'
    do {
      println("Enter a string (type 'exit' to quit):")
      i = readLine().trim.toLowerCase.replaceAll("[^a-z]", "")
      // Checking if the input is not 'exit'
      if (i != "exit") {
        // Calling the checkLapindrome method and printing the result
        val result = checkLapindrome(i)
        println(result)
      }
    } while (i != "exit")
  }

  // Function to check if a string is a lapindrome
  def checkLapindrome(s: String): String = {
    val length = s.length
    // Checking if the length is even
    val isEvenLength = length % 2 == 0
    // Finding the middle index of the string
    val middleIndex = length / 2

    // Extracting the left half of the string
    val leftHalf = s.substring(0, middleIndex)

    // Extracting the right half of the string based on whether it's even or odd length
    val rightHalf = if (isEvenLength) s.substring(middleIndex) else s.substring(middleIndex + 1)

    // Reversing the right half of the string
    val reversedRightHalf = rightHalf.reverse

    // Checking if the left half is equal to the reversed right half
    val isLapindrome = leftHalf == reversedRightHalf

    val middleCharacter = if (isEvenLength) "NA" else s.charAt(middleIndex).toString

    // Constructing the result string based on lapindrome condition
    val result = if (isLapindrome) {
      s"Yes, the string is a lapindrome\n" +
        s"Length of lapindrome: $length\n" +
        s"Middle character: $middleCharacter\n" +
        s"Left half: $leftHalf\n" +
        s"Right half: $rightHalf\n" +
        s"Reversed right half: $reversedRightHalf"
    } else {
      "No, the string is not a lapindrome"
    }

    result
  }
}