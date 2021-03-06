package com.fearke.hexapod;

import java.util.Arrays;

import com.fearke.util.Matrix;
import com.fearke.util.Plane3d;
import com.fearke.util.Vector3d;

public class Body {

	public final static int d = 200;
	public final static int w = 100;
	public final static int wm = 150;
	public final static int h = 50;
	public final static int count = 6;

	public final static Vector3d offset[];
	static {
		offset = new Vector3d[count];
		offset[0] = new Vector3d(d / 2, +w / 2, -h / 2);
		offset[1] = new Vector3d(0, +wm / 2, -h / 2);
		offset[2] = new Vector3d(-d / 2, +w / 2, -h / 2);
		offset[3] = new Vector3d(-d / 2, -w / 2, -h / 2);
		offset[4] = new Vector3d(0, -wm / 2, -h / 2);
		offset[5] = new Vector3d(d / 2, -w / 2, -h / 2);
	}

	private Vector3d center;
	private double[] rotation;

	private Leg[] legs;
	private LegConfig legConfig;

	public Body() {
		legs = new Leg[count];
		for (int i = 0; i < legs.length; ++i) {
			legs[i] = new Leg();
		}

		center = new Vector3d();
		rotation = new double[] { 0, 0, 0 };
	}

	public void init() {
		initLeg(0, 75, -50);
		update();
	}

	public void setRotation(double[] r) {
		this.rotation = r;
	}

	public double[] getRotation() {
		return rotation.clone();
	}

	public double[] getConfig() {
		double[] config = new double[3 * 6];
		int idx = 0;
		for (int i = 0; i < count; ++i) {
			Leg leg = legs[i];
			config[idx++] = leg.getRa();
			config[idx++] = leg.getRb();
			config[idx++] = leg.getRc();
		}
		return config;
	}

	public void setConfig(double[] config) {
		int idx = 0;
		for (int i = 0; i < count; ++i) {
			Leg leg = legs[i];
			leg.setR(Arrays.copyOfRange(config, idx, idx + 3));
			idx += 3;
		}
	}

	public Vector3d getCenter() {
		return center;
	}

	public void setCenter(final Vector3d center) {
		this.center = center;
	}

	public Leg getLeg(int index) {
		return legs[index];
	}

	public LegConfig getLegConfig() {
		return legConfig;
	}

	private void initLeg(double x, double y, double z) {
		final int off = 50;
		final double[][] o = new double[][] { { x + off, y }, { x, y },
				{ x - 2 * off, y }, { x - 2 * off, -y }, { x, -y },
				{ x + off, -y } };
		for (int i = 0; i < count; ++i) {
			legs[i].init(offset[i], o[i][0], o[i][1], z);
		}
	}

	public void update() {
		Matrix m = updateP1();

		for (Leg leg : legs) {
			leg.update(m);
		}
	}

	public Matrix updateP1() {
		Matrix m = Matrix.getMatrix(rotation);
		for (int i = 0; i < count; ++i) {
			Vector3d p = m.multiply(offset[i]);
			p.add(center);

			legs[i].p1 = p;
		}
		return m;
	}

	public void stabalize() {
		updateLegConfig();

		Plane3d p = legConfig.getPlane();
		double distance = p.distance(center);
		double[] r = Matrix.getRotation(p.n);

		center = new Vector3d(0, 0, distance);
		rotation = new double[] { -r[0], -r[1], -r[2] };

		update();

		legConfig.update();
	}

	public void updateInverse() {
		for (Leg leg : legs) {
			leg.updateInverse(0);
		}
	}

	/**
	 * Update most stable leg configuration.
	 */
	public void updateLegConfig() {
		legConfig = calculateLegConfig();
	}

	/**
	 * @return most stable leg configuration.
	 */
	private LegConfig calculateLegConfig() {
		int[][] indices = {

		{ 0, 1, 3 }, { 0, 1, 4 }, { 0, 1, 5 },

		{ 0, 2, 3 }, { 0, 2, 4 }, { 0, 2, 5 },

		{ 1, 2, 3 }, { 1, 2, 4 }, { 1, 2, 5 },

		{ 0, 3, 4 }, { 0, 3, 5 }, { 0, 4, 5 },

		{ 1, 3, 4 }, { 1, 3, 5 }, { 1, 4, 5 },

		{ 2, 3, 4 }, { 2, 3, 5 }, { 2, 4, 5 } };

		for (int i = 0; i < indices.length; ++i) {
			LegConfig lc = new LegConfig(this, indices[i]);
			lc.update();
			if (!lc.isStable()) {
				continue;
			}
			return lc;
		}

		throw new RuntimeException();
	}
}
