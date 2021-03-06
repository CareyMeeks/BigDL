/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl.visualization.tensorboard

import java.util.concurrent.Executors

import org.tensorflow.framework.Summary
import org.tensorflow.util.Event

/**
 * Writes Summary protocol buffers to event files.
 * @param logDirecotry
 * @param flushMillis
 */
class FileWriter(val logDirecotry : String, flushMillis: Int = 10000) {
  private val logDir = new java.io.File(logDirecotry)
  require(!logDir.exists() || logDir.isDirectory, s"FileWriter: can not create $logDir")
  if (!logDir.exists()) logDir.mkdirs()
  private val eventWriter = new EventWriter(logDirecotry, flushMillis)
  val writerDemon = Executors.defaultThreadFactory().newThread(eventWriter)
  writerDemon.setDaemon(true)
  private val pool = Executors.newFixedThreadPool(1)
  pool.submit(writerDemon)

  /**
   * Adds a Summary protocol buffer to the event file.
   * @param summary a Summary protobuf String generated by bigdl.utils.Summary's
   *                scalar()/histogram().
   * @param globalStep a consistent global count of the event.
   * @return
   */
  def addSummary(summary: Summary, globalStep: Long): this.type = {
    val event = Event.newBuilder().setSummary(summary).build()
    addEvent(event, globalStep)
    this
  }

  /**
   * Add a event protocol buffer to the event file.
   * @param event A event protobuf contains summary protobuf.
   * @param globalStep a consistent global count of the event.
   * @return
   */
  def addEvent(event: Event, globalStep: Long): this.type = {
    eventWriter.addEvent(
      event.toBuilder.setWallTime(System.currentTimeMillis() / 1e3).setStep(globalStep).build())
    this
  }

  /**
   * Close file writer.
   * @return
   */
  def close(): Unit = {
    eventWriter.close()
    pool.shutdown()
  }
}
