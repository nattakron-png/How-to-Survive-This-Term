import test from 'node:test'
import assert from 'node:assert/strict'

import { players } from '../src/mock/players.js'
import { tournamentTeams } from '../src/mock/tournaments.js'
import { captureRoster, getRegisteredPlayers } from '../src/mock/rosters.js'

test('old tournament keeps its player after live roster changes; new tournament sees the new roster', () => {
  const registration = tournamentTeams[0]
  const original = getRegisteredPlayers(registration.tournamentId, registration.teamId)
  assert.ok(original.length > 0)
  const changed = players.find((player) => player.id === original[0].id)
  const previousName = changed.name
  const previousTeamId = changed.teamId

  try {
    changed.name = 'Changed later'
    changed.teamId = null
    captureRoster(99999, registration.teamId)

    assert.equal(getRegisteredPlayers(registration.tournamentId, registration.teamId)[0].name, previousName)
    assert.equal(getRegisteredPlayers(registration.tournamentId, registration.teamId).length, original.length)
    assert.equal(getRegisteredPlayers(99999, registration.teamId).length, original.length - 1)
  } finally {
    changed.name = previousName
    changed.teamId = previousTeamId
  }
})
