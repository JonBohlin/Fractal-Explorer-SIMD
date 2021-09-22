# Fractal-Explorer-SIMD

Exactly the same as Fractal-Explorer on the outside with some major enhancements on the inside resulting in a substantial speed-up: utilization of SIMD hardware that has just become available for Java 17 in "incubation" (could potentially also work with Java 16). Should be generic for all SIMD-hardware supported by Java without change.

This was very much inspired by the excellent video on C++ intrinsics for the Mandelbrot fractal by javidx9: https://youtu.be/x9Scb5Mku1g

To compile (requires Java 17 - has not been tested on Java 16):
javac --add-modules jdk.incubator.vector Explorer.java FractalExplorer.java JuliaFractalJPanel.java MandelbrotFractalJPanel.java

Run:

java --add-modules jdk.incubator.vector FractalExplorer

In IntelliJ IDEA go first to Run (menu), choose Run and Edit Configurations, navigate to "Modify options", then click "Add VM options" and add --add-modules jdk.incubator.vector
Next, go to the File menu, then Settings, choose Java Compiler and add --add-modules jdk.incubator.vector
