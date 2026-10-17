package com.music.innertube

import com.music.innertube.YouTube.SearchFilter
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

class SearchVideoTest {
  @Ignore("Live YouTube integration check; requires external network access and is not a deterministic unit test.")
  @Test
  fun testVideoSearch() {
    runBlocking {
      val result = YouTube.search("fakira", SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D"))
      println("Result: $result")
      if (result.isSuccess) {
        println("Items size: ${result.getOrNull()?.items?.size}")
        result.getOrNull()?.items?.forEach { println("Item: $it") }
      } else {
        println("Error: ${result.exceptionOrNull()}")
      }
    }
  }
}
