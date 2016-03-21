package sanskritnlp.dictionary

import java.io.{File, PrintWriter}

import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.io.{BufferedSource, Source}

class BabylonDictionary(name_in: String, source_in: String = "", head_language: String) {
  var wordToLocations: HashMap[String, ListBuffer[Int]] = new HashMap[String, ListBuffer[Int]]
  var wordToMeanings = new HashMap[String, ListBuffer[String]]
  val log = LoggerFactory.getLogger(this.getClass)

  val dict_name = name_in
  val source = source_in

  // Maintained by external users of this dictionary - not here. Kept here for conveneince.
  val wordsSeen = new mutable.HashSet[String]
  var start_word_index: Int = 1
  var end_word_index: Int = 100000
  var word_index = 0

  var fileLocation = ""
  var linesIter: Iterator[String] = null
  var src: Source = null

  def fromFile(infileStr: String) = {
    // log info s"Reading $infileStr for $dict_name"
    fileLocation = infileStr
    word_index = 0
    src = Source.fromFile(infileStr, "utf8")
    linesIter = src.getLines
  }

  def hasNext(): Boolean = {
    return linesIter.hasNext
  }

  def next(): (Array[String], String) = {
    val returnTuple = (linesIter.next().split('|'), linesIter.next)
    assert(linesIter.next() == "")
    return returnTuple
  }

  def take(entriesToSkip: Int) = {
    while(hasNext() && word_index < entriesToSkip) {
      next()
      word_index = word_index + 1
    }
  }

  def makeWordToLocationMap(headword_pattern: String = ".+") = {
    log info s"Making wordToLocationMap for $dict_name"
    word_index = 0
    while (hasNext()) {
      val (headwords, meaning) = next()
      word_index = word_index + 1
      // log.info(s"word_index : $word_index")
      val filtered_headwords = headwords.filter(_ matches headword_pattern)
      filtered_headwords.foreach(word => {
        val locus_list = wordToLocations.getOrElse(word, ListBuffer[Int]())
        locus_list += word_index
        wordToLocations += (word -> locus_list)
      })
    }
    fromFile(fileLocation)
  }

  def getMeaningAtIndex(locus: Int): String = {
    // log info(s"locus: $locus")
    take(locus - 1)
    val (_, meaning_line) = next()
    fromFile(fileLocation)
    return meaning_line
  }

  def getMeanings(word: String): ListBuffer[String] = {
    if (wordToMeanings.size == 0) {
      if (wordToLocations.size == 0) {
        makeWordToLocationMap()
      }
      val definition_locus_list = wordToLocations.getOrElse(word, ListBuffer[Int]())
      return definition_locus_list.map(getMeaningAtIndex(_))
    } else {
      return wordToMeanings.getOrElse(word, null)
    }
  }

  def getWords: Iterable[String] = {
    if (wordToMeanings.size == 0) {
      return wordToLocations.keys
    } else {
      return wordToMeanings.keys
    }

  }

  def makeWordToMeaningsMap(headword_pattern: String = ".+") = {
    log info s"Making wordToMeanings for $dict_name"
    word_index = 0
    while (hasNext()) {
      val (headwords, meaning) = next()
      word_index = word_index + 1
      // log.info(s"word_index : $word_index")
      val filtered_headwords = headwords.filter(_ matches headword_pattern)
      filtered_headwords.foreach(word => {
        val meaningList = wordToMeanings.getOrElse(word, ListBuffer[String]())
        meaningList += meaning
        wordToMeanings += (word -> meaningList)
      })
    }
  }

}

object babylonDictTest {
  val log = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    val kalpadruma = new BabylonDictionary(name_in = "कल्पद्रुमः", source_in = "http://www.sanskrit-lexicon.uni-koeln.de/scans/csldoc/contrib/index.html", head_language = "sa")
    kalpadruma.fromFile(infileStr = "/home/vvasuki/stardict-sanskrit/sa-head/kalpadruma-sa/kalpadruma-sa.babylon_final")
    log info kalpadruma.getMeanings("इ").mkString("\n\n")
    log info kalpadruma.getMeanings("अ").mkString("\n\n")
    log info kalpadruma.getMeanings("उ").mkString("\n\n")
  }
}