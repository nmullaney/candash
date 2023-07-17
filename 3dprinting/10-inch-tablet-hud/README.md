# 10" Tablet HUD

Use a relatively cheap OLED 10" tablet as a HUD for CANdash.

This uses just the top half of the tablet's screen to achieve an extra wide aspect-ratio so that not much of your view is blocked, and the focal distance is as far away as possible. It's not a real HUD as it doesn't reflect of the car's windshield and the focal distance is still a bit too close.

## BOM

- [Galaxy Tab S5e](https://www.amazon.com/Samsung-Galaxy-Tablet-Black-Renewed/dp/B07V2PDH6T/), or other 10" OLED tablet (CAD editing required)
- 1/8" Acrylic first-surface mirror, at least 6" by 9". [2-way mirror](https://www.amazon.com/gp/product/B09QQQZN3F) can work too. Cut by scoring and snapping.
- Right angle usb-c connector, [this one](https://www.amazon.com/gp/product/B07VJN9YQM) fits nicely.
- [Neoprene foam sheet](https://www.amazon.com/Neoprene-Adhesive-Padding-Multi-Function-Soundproof/dp/B08RB23HBQ) to cover back of mirror, and optionally layer entire exterior of print under vinyl wrap for a soft-touch wrap. (Experiment first with your foam and wrap on some scrap prints to test how it looks and feels)
- Matte black leather vinyl wrap to match dashboard, or other vinyl wrap of your choice.
- Print in PETG or other high-temp filament, sand surfaces before wrapping.

## CAD instructions

TL;DR: It's fully parametric: Modify -> Change Parameters

Attached is a Fusion 360 archive file (.f3d) so you can edit the design as necessary. For most tweaks, you shouldn't need to edit any sketches or features, just go to Modify -> Change Parameters (in the toolbar) to tweak dimensions of the mirror, tablet, and eye position. Read the comments of the parameters.

There is a sketch (sketch 7 iirc) that shows the upper and lower limits of your line-of-sight from your eyeballs to the tablet screen...this is used to set the height of the mirror (not the parameters!). To adjust, place the tablet on your dash where you want to put it, measure the direct distance from the closest edge of the tablet to your eyes. For elevation, use a straight reference to extend the plane of the tablet screen to your chest, measure at a right angle upwards to your eyes. Reference the sketch if needed for clarity on what to measure.

The mirror width should be larger than the display itself, but smaller than the width of the tablet.

## Print instructions

In Fusion, use Utilities -> Make to export the walls and shields to your slicer.

Everything is designed to print laying flat for simplicity. The walls print with the outside surface against the build plate. The shield prints with the inner surface against the build plate (unless you can achieve good overhangs then you could try it outside-down for a better outer surface).

No supports needed.

Use material that won't melt in the sun under your windshield.

## Assembly instructions

### Side walls

Dry fit and ensure the front edge of the tablet is fully seated to the front of the slot. Test fit the mirror and tablet with a single wall to ensure your angle is good. Test your USB cable fits and charges the tablet (some cables don't go deep enough to get through the housing).

Sand the surfaces of the prints and clean, line the inside surface with the thin neoprene or other very dark material to reduce glare. Wrap the outside surfaces if desired.

Use some thin double-sided tape or a dab of hot glue at each end to hold the side walls onto the edges of the tablet. If using tape, put the tape on the bottom of the tablet, then slide the printed walls over the tablet.

### Shield

Lots of tolerance was designed into the slots for the shield so that you can snap it into place after assembling everything else. You'll want to be able to install/remove it even after final assembly to have access to the screen in case you need to tweak settings.

### Mirror

If using a 2-way mirror, you want to find the side that provides a "first surface" reflection. Do this after you peel the film by holding it at a 45 degree angle over some high contrast lines on paper, or over your tablet running CANdash. If you see ghost or double images, flip it over the other way. The reflection should be clear with only 1 image.

Cut the mirror and test fit before removing the film. Cut only the width first to fit in the housing, then after it's in the housing, use masking tape to mark the height so it's flush with the top of the housing. Use a metal straight edge and a new, sharp razor blade in a utility knife. Measure carefully and clamp the straight edge onto the mirror and your workdesk. It's a good idea to put the straight edge over the part you want to keep, so that if you slip, you scratch up the waste and not the good half. Score several times using moderate pressure (don't press so hard that you slip). Line up the score in the sheet over the edge of your work desk, with a hand on each side of the score, push FAST and hard to snap it. If it bounces back...you're either wussing out, or you need to score it more.

After cutting, optionally sand the cut edges using full sheets of sand paper on a flat surface. Start at 100 grit and work up to 800 wet. For the top edge, after sanding, you can sharpie or paint this edge black, as it's an exposed edge after assembly.

If using a 2-way mirror, you can try it out without wrapping the back, so that it's transparent. If you find the tablet isn't bright enough during the day to read the gauges, you can use the neoprene sheet to cover the top of the mirror to block light.

## Configuring CANdash

There are 3 things you'll want to configure in CANdash's settings:

- Always dark mode
- Blackout screen w/ center display
- HUD mode: half-size (Coming soon)
