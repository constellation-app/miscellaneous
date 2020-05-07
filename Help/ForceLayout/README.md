# Java port of d3-layout

This NetBeans project is a Java port of [d3-force](https://github.com/d3/d3-force).

Implemented so far:
* [d3-quadtree](https://github.com/d3/d3-quadtree/) + unit tests
* forceSimulation
* forceCenter
* forceManyBody

Using the example at [Force directed graph: minimal working example](https://tomroth.com.au/fdg-minimal/) down to "You should be able to see something happening now on your force directed graph - six red dots in a rough pentagon shape", the Java equivalent is this.

```
    @Test(description="basic many body")
    public void forceManyBody() {
        final List<IVertex> vxs = new ArrayList();
        for(int i=0; i<6; i++) {
            vxs.add(v(Double.NaN , Double.NaN));
        }
        System.out.printf("%s\n", vxs);
        final Simulation sim = new Simulation(vxs);
        sim.addForce("many_body", new ForceManyBody());
        sim.addForce("centre", new ForceCentre(960/2, 600/2));
        sim.step();
    }
```

The result is exactly the same as the Javascript equivalent: comparing the Javascript resulting x,y points with the Java resulting x,y points gives the same numbers.

CONSTELLATION typically works with floats, but using float x,y variables causes divergence from the Javascript code that only uses (the equivalent of) doubles. Therefore, doubles are used everywhere so the Java code can produce the same results as the Javascript code, so I have greater confidence that the port works.

To do:
* forceLinks
* Javadoc
* CONSTELLATION integration
