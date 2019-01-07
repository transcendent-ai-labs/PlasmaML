package io.github.mandar2812.PlasmaML.helios.core

import io.github.mandar2812.dynaml.tensorflow._
import org.platanios.tensorflow.api._
import org.platanios.tensorflow.api.ops.NN.SameConvPadding

/**
  * A collection of architectures building blocks
  * for learning and predicting from solar images.
  *
  * */
object Arch {


  /**
    * CNN architecture for GOES XRay flux
    * */
  private[PlasmaML] val cnn_goes_v1 = {
    tf.learn.Cast("Input/Cast", FLOAT32) >>
      dtflearn.conv2d_unit(Shape(2, 2, 4, 64), (1, 1))(0) >>
      dtflearn.conv2d_unit(Shape(2, 2, 64, 32), (2, 2))(1) >>
      dtflearn.conv2d_unit(Shape(2, 2, 32, 16), (4, 4))(2) >>
      dtflearn.conv2d_unit(Shape(2, 2, 16, 8), (8, 8), dropout = false)(3) >>
      tf.learn.MaxPool("MaxPool_3", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      tf.learn.Flatten("Flatten_3") >>
      tf.learn.Linear("FC_Layer_4", 128) >>
      tf.learn.ReLU("ReLU_4", 0.1f) >>
      tf.learn.Linear("FC_Layer_5", 64) >>
      tf.learn.ReLU("ReLU_5", 0.1f) >>
      tf.learn.Linear("FC_Layer_6", 8) >>
      tf.learn.Sigmoid("Sigmoid_6") >>
      tf.learn.Linear("OutputLayer", 1)
  }

  private[PlasmaML] val cnn_goes_v1_1 = {
    tf.learn.Cast("Input/Cast", FLOAT32) >>
      dtflearn.conv2d_unit(Shape(2, 2, 4, 64), (1, 1))(0) >>
      dtflearn.conv2d_unit(Shape(2, 2, 64, 32), (2, 2))(1) >>
      dtflearn.conv2d_unit(Shape(2, 2, 32, 16), (4, 4))(2) >>
      dtflearn.conv2d_unit(Shape(2, 2, 16, 8), (8, 8))(3) >>
      dtflearn.conv2d_unit(Shape(2, 2, 8, 4), (16, 16), dropout = false)(4) >>
      tf.learn.MaxPool("MaxPool_3", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      tf.learn.Flatten("Flatten_3") >>
      tf.learn.Linear("FC_Layer_4", 64) >>
      tf.learn.SELU("SELU_5") >>
      tf.learn.Linear("FC_Layer_5", 8) >>
      tf.learn.Sigmoid("Sigmoid_5") >>
      tf.learn.Linear("OutputLayer", 1)
  }

  private[PlasmaML] val cnn_goes_v1_2 = {
    tf.learn.Cast("Input/Cast", FLOAT32) >>
      dtflearn.conv2d_pyramid(2, 4)(6, 3)(0.01f, dropout = true, 0.6f) >>
      tf.learn.MaxPool("MaxPool_3", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      dtflearn.conv2d_unit(Shape(2, 2, 8, 4), (16, 16), dropout = false, relu_param = 0.01f)(4) >>
      tf.learn.MaxPool("MaxPool_5", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      tf.learn.Flatten("Flatten_5") >>
      dtflearn.feedforward(128)(6) >>
      dtflearn.Tanh("Tanh_6") >>
      dtflearn.feedforward(64)(7) >>
      dtflearn.Tanh("Tanh_7") >>
      dtflearn.feedforward(32)(8) >>
      dtflearn.Tanh("Tanh_6") >>
      tf.learn.Linear("OutputLayer", 1)
  }

  private[PlasmaML] val cnn_xray_class_v1 = {
    tf.learn.Cast("Input/Cast", FLOAT32) >>
      dtflearn.conv2d_pyramid(2, 4)(7, 3)(0.1f, dropout = true, 0.6f) >>
      dtflearn.conv2d_unit(Shape(2, 2, 8, 4), (16, 16), dropout = false)(4) >>
      tf.learn.MaxPool("MaxPool_6", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      tf.learn.Flatten("Flatten_6") >>
      dtflearn.feedforward(128)(7) >>
      tf.learn.SELU("SELU_7") >>
      dtflearn.feedforward(64)(8) >>
      tf.learn.SELU("SELU_8") >>
      dtflearn.feedforward(32)(9) >>
      tf.learn.SELU("SELU_9") >>
      dtflearn.feedforward(4)(10)
  }

  private[PlasmaML] val cnn_sw_v1 = {
    tf.learn.Cast("Input/Cast", FLOAT32) >>
      dtflearn.conv2d_unit(Shape(2, 2, 4, 64), (1, 1))(0) >>
      dtflearn.conv2d_unit(Shape(2, 2, 64, 32), (2, 2))(1) >>
      dtflearn.conv2d_unit(Shape(2, 2, 32, 16), (4, 4))(2) >>
      dtflearn.conv2d_unit(Shape(2, 2, 16, 8), (8, 8), dropout = false)(3) >>
      tf.learn.MaxPool("MaxPool_3", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      tf.learn.Flatten("Flatten_3") >>
      tf.learn.Linear("FC_Layer_4", 128) >>
      tf.learn.SELU("SELU_4") >>
      tf.learn.Linear("FC_Layer_5", 64) >>
      tf.learn.SELU("SELU_5") >>
      tf.learn.Linear("FC_Layer_6", 8) >>
      tf.learn.SELU("SELU_6") >>
      tf.learn.Linear("OutputLayer", 2)
  }

  /**
    * Dynamic causal time lag inference architecture. v 2.0
    *
    * @param sliding_window Size of finite time horizon for causal inference
    * @param mo_flag Set to true for training one model for each output.
    * @param prob_timelags Set to true if predicting causal time lag in a probabilistic manner.
    * */
  private[PlasmaML] def cnn_sw_v2(
    sliding_window: Int,
    mo_flag: Boolean,
    prob_timelags: Boolean) = {

    val num_pred_dims =
      if(!mo_flag) 2
      else if(mo_flag && !prob_timelags) sliding_window + 1
      else if(!mo_flag && prob_timelags) sliding_window + 1
      else 2*sliding_window

    tf.learn.Cast("Input/Cast", FLOAT32) >>
      dtflearn.conv2d_unit(Shape(2, 2, 4, 64), (1, 1))(0) >>
      dtflearn.conv2d_unit(Shape(2, 2, 64, 32), (2, 2))(1) >>
      dtflearn.conv2d_unit(Shape(2, 2, 32, 16), (4, 4))(2) >>
      dtflearn.conv2d_unit(Shape(2, 2, 16, 8), (8, 8), dropout = false)(3) >>
      tf.learn.MaxPool("MaxPool_3", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      tf.learn.Flatten("Flatten_3") >>
      tf.learn.Linear("FC_Layer_4", 128) >>
      dtflearn.Phi("Act_4") >>
      tf.learn.Linear("FC_Layer_5", 64) >>
      dtflearn.Phi("Act_5") >>
      tf.learn.Linear("FC_Layer_6", num_pred_dims)
  }

  private[PlasmaML] val cnn_sw_dynamic_timescales_v1 = {
    tf.learn.Cast("Input/Cast", FLOAT32) >>
      dtflearn.conv2d_pyramid(2, num_channels_input = 4)(7, 3)(0.1f, true, 0.6f) >>
      dtflearn.conv2d_unit(Shape(2, 2, 8, 4), (16, 16), dropout = false)(4) >>
      tf.learn.MaxPool("MaxPool_6", Seq(1, 2, 2, 1), 1, 1, SameConvPadding) >>
      tf.learn.Flatten("Flatten_6") >>
      dtflearn.feedforward(128)(7) >>
      tf.learn.SELU("SELU_7") >>
      dtflearn.feedforward(64)(8) >>
      tf.learn.SELU("SELU_8") >>
      dtflearn.feedforward(32)(9) >>
      tf.learn.Sigmoid("Sigmoid_9") >>
      tf.learn.Linear("OutputLayer", 3)
  }

}