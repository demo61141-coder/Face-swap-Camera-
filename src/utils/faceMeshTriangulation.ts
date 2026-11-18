export const INNER_LIP_RING = [
  78, 191, 80, 81, 82, 13, 312, 311, 310, 415,
  308, 324, 318, 402, 317, 14, 87, 178, 88, 95,
];

export const FACE_OVAL_RING = new Set([
  10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
  397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
  172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
]);

const INNER_LIP_SET = new Set(INNER_LIP_RING);

export function buildFaceMeshTriangles(
  tesselationEdges: Array<{ start: number; end: number }>,
): Uint16Array {
  const adj = new Map<number, Set<number>>();
  for (const { start, end } of tesselationEdges) {
    if (start === end) continue;
    if (!adj.has(start)) adj.set(start, new Set());
    if (!adj.has(end)) adj.set(end, new Set());
    adj.get(start)!.add(end);
    adj.get(end)!.add(start);
  }

  const triangles: number[] = [];
  const seen = new Set<string>();

  for (const [u, uNeighbors] of adj.entries()) {
    for (const v of uNeighbors) {
      if (v <= u) continue;
      const vNeighbors = adj.get(v);
      if (!vNeighbors) continue;
      for (const w of vNeighbors) {
        if (w <= v) continue;
        if (!uNeighbors.has(w)) continue;
        // Skip triangles strictly bridging across the inner mouth opening
        if (INNER_LIP_SET.has(u) && INNER_LIP_SET.has(v) && INNER_LIP_SET.has(w)) {
          continue;
        }
        const key = `${u},${v},${w}`;
        if (!seen.has(key)) {
          seen.add(key);
          triangles.push(u, v, w);
        }
      }
    }
  }

  return new Uint16Array(triangles);
}
