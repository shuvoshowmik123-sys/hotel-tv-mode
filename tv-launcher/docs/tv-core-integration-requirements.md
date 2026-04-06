# TV Core Integration Requirements

This note captures the non-negotiable TV-system requirements for the launcher rebuild.

## Goal

If the launcher is rebuilt or replaced from stock, it must still behave like a real TV launcher and remain connected to the TV platform features already present in firmware.

## Required TV Core Features

- HDMI inputs must be visible in the launcher.
- The launcher must show which physical/TV inputs are available on the device.
- The launcher must reflect the current TV connection state clearly.
- The launcher must understand and expose communication between:
  - TV input
  - HDMI input
  - other available input sources on the device
- TV screen/source layout must remain understandable and usable from the launcher.
- Input switching must feel native to the TV, not like a disconnected third-party app.

## Rebuild Rule

Any rebuilt launcher must preserve or re-implement the stock TV integration layer before visual polish is considered complete.

This means the launcher cannot be treated as only a content shell. It must stay aware of:

- source availability
- source status
- active input selection
- TV input to launcher communication
- launcher to TV input switching behavior

## Architecture Reminder

When rebuilding from stock or designing a replacement:

- keep the TV navigation/focus system tied to the actual TV hardware behavior
- keep the source/input model connected to the launcher UI
- do not ship a launcher that loses stock TV input visibility or HDMI/source awareness

## Product Constraint

The launcher is not only an app launcher. It is also a TV control surface.

That means HDMI, TV inputs, and source communication are core product requirements, not optional add-ons.
