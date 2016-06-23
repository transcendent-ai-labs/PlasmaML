
// Test a GP model
CRRESTest(
  new RBFKernel(1.5), new DiracKernel(0.6),
  1000, 2000, 2, 0.01
)

// Test a feed forward neural network
CRRESTest(
  1, List("tansig", "linear"), List(12),
  7000, 3000, 0.2, 120,
  0.5, 0.00, 0.75
)
