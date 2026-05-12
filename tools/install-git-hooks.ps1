$ErrorActionPreference = "Stop"

$repoRoot = git rev-parse --show-toplevel
if (-not $repoRoot) {
    throw "Impossible de trouver la racine Git."
}

$hooksPath = Join-Path $repoRoot "tools/git-hooks"
if (-not (Test-Path -LiteralPath $hooksPath)) {
    throw "Dossier de hooks introuvable: $hooksPath"
}

git config extensions.worktreeConfig true
git config --worktree core.hooksPath tools/git-hooks
Write-Host "Hooks Git configures pour ce worktree: tools/git-hooks"
