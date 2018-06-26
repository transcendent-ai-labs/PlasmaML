package io.github.mandar2812.PlasmaML.dynamics.mhd

import breeze.linalg.DenseMatrix
import io.github.mandar2812.dynaml.utils
import io.github.mandar2812.dynaml.analysis.implicits._
import io.github.mandar2812.dynaml.tensorflow.dtf
import org.platanios.tensorflow.api._
import org.platanios.tensorflow.api.learn.Mode
import org.platanios.tensorflow.api.learn.layers.Layer
import org.platanios.tensorflow.api.ops.variables.{Initializer, RandomUniformInitializer}

case class UpwindTF(
  override val name: String, rDomain: (Double, Double),
  nR: Int, nTheta: Int, omegaInit: Initializer = RandomUniformInitializer(0.01, 1.0))
  extends Layer[Output, Output](name) {

  override val layerType: String = s"Upwind1D[r:$rDomain, nR:$nR, nTheta:$nTheta]"

  /**
    * The Carrington longitude lies
    * between 0 and 2*Pi radians.
    * */
  val thetaDomain: (Double, Double) = (0d, 2*math.Pi)

  protected val (deltaR, deltaTheta) = (
    (rDomain._2 - rDomain._1)/nR,
    (thetaDomain._2 - thetaDomain._1)/nTheta
  )


  override protected def _forward(input: Output)(implicit mode: Mode): Output = {

    val dv_dtheta = tf.constant(
      dtf.tensor_f32(nTheta, nTheta)(DenseMatrix.tabulate(nTheta, nTheta)(
        (i, j) => if(i == j) -1.0 else if(j == (i+1)%nTheta) 1.0 else 0.0).t.toArray:_*),
      FLOAT64, Shape(nTheta, nTheta),
      "DeltaV")


    val omega_rot = tf.variable("OmegaRot", input.dataType, Shape(), omegaInit)

    val velocity_profile = tf.stack(
      (1 to nR).scanLeft(input)((x, _) => {
        val invV = x.pow(-1d)
          x.add(x.tensorDot(dv_dtheta, Seq(1), Seq(0)).multiply(invV)
            .multiply(omega_rot)
            .multiply(deltaR/deltaTheta))
      }),
      axis = -1)

    val sun_earth_velocity = velocity_profile(::, 0, ---)

    val rH = 50.0
    val alpha = 0.15
    val r = tf.constant(
      dtf.tensor_f64(nR + 1)(
        (utils.range(rDomain._1, rDomain._2, nR):+rDomain._2):_*
      ).divide(rH),
      FLOAT64,
      Shape(nR + 1))

    val v_acc = tf
      .stack(Seq.fill(nR + 1)(sun_earth_velocity(::, 0).multiply(alpha)), axis = 1)
      .multiply(r.multiply(-1.0).exp.multiply(-1.0).add(1.0))

    sun_earth_velocity.add(v_acc)
  }
}

case class UpwindPropogate(
  override val name: String,
  rDomain: (Double, Double),
  nR: Int, nTheta: Int,
  omegaInit: Initializer = RandomUniformInitializer())
  extends Layer[Output, Output](name) {

  override val layerType: String = s"UpwindPropogate[r:$rDomain, nR:$nR, nTheta:$nTheta]"

  /**
    * The Carrington longitude lies
    * between 0 and 2*Pi radians.
    * */
  val thetaDomain: (Double, Double) = (0d, 2*math.Pi)

  protected val (deltaR, deltaTheta) = (
    (rDomain._2 - rDomain._1)/nR,
    (thetaDomain._2 - thetaDomain._1)/nTheta
  )

  override protected def _forward(input: Output)(implicit mode: Mode): Output = {

    val velocities = input(::, 0, ::)

    val sliding_avg_tensor = dtf.tensor_f32(
      nR, nR + 1)(
      DenseMatrix.tabulate(nR, nR + 1)(
        (i, j) => if(i == j) 0.5 else if(j == (i+1)) 0.5 else 0.0
      ).t.toArray:_*
    )

    val sliding_avg = tf.constant(
      sliding_avg_tensor,
      FLOAT64, Shape(nR, nR+1), "VAvgOp")

    val deltat = velocities.tensorDot(sliding_avg, Seq(1), Seq(0))
      .pow(-1d)
      .multiply(deltaR)
      .sum()

    val v = input(::, 0, -1).reshape(Shape())

    tf.stack(Seq(v, deltat), axis = -1).reshape(Shape(2))
  }
}
