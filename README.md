![Intelligent Scissors. One half of a leaf shows the Sobel gradient and the other shows the computed contour.](live-wire-cover.png?v=7f6c8cc)

**Intelligent Scissors** is a Java Swing tool for cutting objects out of images. Click a point on an object's outline, then move the cursor along it. A red line follows the edges between your anchor and the cursor. Close the outline to save the selection as a transparent PNG.

[How it works](#how-it-works) · [Run the app](#run-the-app) · [Try the leaf sample](#try-the-leaf-sample)

## How it works

![Four views of the same leaf: the source image, its gradient, the shortest paths joining eight anchors, and the cutout on a checkerboard.](live-wire-stages.png?v=31b504f)

The project's Java code produced the gradient map, contour, and cutout from this [320 × 320 leaf image](leaf-sample.png). The eight white dots are the anchors used for this example. The red line joins them, and the checkerboard shows where the output is transparent.

| Stage | What to look for | Implementation |
| --- | --- | --- |
| Find edges | Sobel filters measure horizontal and vertical changes in brightness. The leaf's outer edge lights up, and its veins appear as fainter lines. | [ImageProcessor.java](dsbb_project/src/ImageProcessor.java) |
| Trace the outline | Each pixel connects to up to eight neighbors. Moving to a neighbor costs `1 / (1 + gradient magnitude)`, so a strong edge is cheap to follow. Dijkstra's algorithm finds the path with the lowest total cost from the anchor to the cursor. | [PathFinder.java](dsbb_project/src/PathFinder.java) |
| Make the cutout | The connected paths form a polygon. Pixels inside it are copied onto a transparent canvas with the same dimensions as the source image. | [CroppingUtils.java](dsbb_project/src/CroppingUtils.java) |

**Where should you place the next anchor?** Both a vein and the outer rim can register as edges. If the red line starts to follow a vein, move back along the rim and click just before the line turns inward. The next segment will start there.

## Run the app

Install **JDK 11 or later** and start in the repository root. The required `algs4.jar` is included.

**Windows PowerShell**

```powershell
cd dsbb_project
New-Item -ItemType Directory -Force build | Out-Null
$sources = (Get-ChildItem src -Filter '*.java').FullName
javac -encoding UTF-8 -cp algs4/algs4.jar -d build @sources
java -cp "build;algs4/algs4.jar" IntelligentScissorsGUI
```

<details>
<summary>macOS and Linux</summary>

```sh
cd dsbb_project
mkdir -p build
javac -encoding UTF-8 -cp algs4/algs4.jar -d build src/*.java
java -cp "build:algs4/algs4.jar" IntelligentScissorsGUI
```

</details>

## Try the leaf sample

<img src="workbench.png?v=1283548" width="426" alt="English-labeled preview of the Swing app tracing the lower left edge of the leaf in red.">

1. Open **File → Open Image** (文件 → 打开图像) and select `leaf-sample.png` from the repository root.
2. Click the leaf's edge to place your first anchor. Move the cursor along the rim and click near bends to fix each segment in place.
3. Return near the first anchor and double-click to close the outline. In the cutout window, click **Save** (保存) and give the file a `.png` extension.

For this version, use a **square image**. The pathfinder initializes the first dimension of its arrays using the image height. On rectangular inputs this can leave paths incomplete or cause an out-of-bounds error.
