---
name: "Bug Report"
about: "Report something TerraReforged does wrong."
title: "[Bug] <Put your title here>"
labels: "Type: Bug, Status: Pending"
assignees: ""

---

## Pre-Issue Checklist

<!--
  Work through each item before submitting. An issue that skips them takes several round trips to
  become actionable, and may be closed as incomplete.

    - Only the latest build is supported.
    - Search the issue tracker first, including closed issues.
    - Check that TerraReforged is the cause. Reproduce it without TerraReforged installed, or with
      the plugins you suspect removed.
    - Check that it applies to the plugin rather than to one config pack or one addon. A pack that
      fails to load is usually a problem with the pack.
    - Attach the whole latest.log. An exception on its own is not enough: what happened before it is
      usually what caused it.
    - Do not ask for compatibility with a specific other plugin. That belongs in an addon. General
      capability, such as reading features from a parent biome, is in scope here.

  Put an x in each box you have completed, like this: [x]
  The Preview tab above shows how your issue will render before you submit it.
-->

- [ ] I am on the latest build of TerraReforged.
- [ ] I have searched the issue tracker, including closed issues.
- [ ] I have checked that another plugin is not the cause.
- [ ] I have checked that this is a problem with the plugin rather than with the config pack I am
  using.
- [ ] I have attached the whole `latest.log`.
- [ ] I have filled out the environment table below.

## Environment

| Name | Value |
|---|---|
| TerraReforged version | <!-- e.g. 7.0.0-BETA, or the jar file name --> |
| Server and build | <!-- e.g. Purpur 26.2 build 2633, or Paper 26.2 build 129 --> |
| Java version | <!-- output of `java -version` --> |
| Other plugins installed | <!-- one line, no line breaks --> |
| Config packs in use | <!-- `/packs` lists them --> |
| Addons in use | <!-- `/addons` lists them --> |

## Issue Description

<!-- What goes wrong, in a sentence or two. -->

### Steps to reproduce

<!-- What you were doing when it happened, including anything beforehand that may have caused it. -->

1. <!-- Put step #1 here. -->
2. <!-- Put step #2 here. -->
3. <!-- etc.              -->

### Expected behavior

<!-- What you expected to happen. -->

### Actual behavior

<!-- What happened instead. -->

### Full stacktrace

<details>
<summary>Exception stacktrace</summary>

<!--
  Paste the exception here, in addition to attaching latest.log. Console output or latest.log has it.
-->

```

```

</details>

### Additional details

<!-- Anything else that helps. A world seed, a pack, or a region file often does. -->
