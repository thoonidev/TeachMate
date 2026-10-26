/* Native details remains usable if JavaScript is unavailable. */
document.addEventListener("click", (event) => {
  document.querySelectorAll(".tm-account[open]").forEach((account) => {
    if (!account.contains(event.target)) account.open = false;
  });
});

document.addEventListener("keydown", (event) => {
  if (event.key !== "Escape") return;
  document.querySelectorAll(".tm-account[open]").forEach((account) => {
    const hadFocus = account.contains(document.activeElement);
    account.open = false;
    if (hadFocus) account.querySelector("summary").focus();
  });
});

// Units are explicit and remain usable without JavaScript; the server validates all ranges.
document.querySelectorAll("[data-duration-for]").forEach((unit) => {
  const input = document.getElementById(unit.dataset.durationFor);
  const updateRange = () => {
    const divisor = unit.value === "horas" ? 60 : 1;
    input.min = Number(input.dataset.durationMin) / divisor;
    input.max = Number(input.dataset.durationMax) / divisor;
  };
  updateRange();
  unit.addEventListener("change", updateRange);
});
