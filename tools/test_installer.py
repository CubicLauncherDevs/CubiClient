"""Contract tests for the launcher descriptor, without touching a user's launcher."""
import unittest

from install import make_descriptor
from common import VERSION


class DescriptorTests(unittest.TestCase):
    def test_vanilla_contract_is_preserved(self):
        base = {"id": "1.8.9", "mainClass": "net.minecraft.client.main.Main",
                "minecraftArguments": "--username ${auth_player_name} --accessToken ${auth_access_token}",
                "libraries": [{"name": "original:library:1"}],
                "assetIndex": {"id": "1.8", "sha1": "unchanged"},
                "downloads": {"client": {"sha1": "original"}},
                "logging": {"client": {"argument": "keep-me"}}}
        own = {"name": "dev.cubi:cubiclient:" + VERSION}
        descriptor = make_descriptor(base, own, [{"name": "net.minecraft:launchwrapper:1.12"}])
        self.assertEqual(descriptor["mainClass"], "net.minecraft.launchwrapper.Launch")
        self.assertEqual(descriptor["libraries"][0], own)
        self.assertIn("${auth_access_token}", descriptor["minecraftArguments"])
        self.assertIn("--tweakClass dev.cubi.launch.CubiTweaker", descriptor["minecraftArguments"])
        for field in ("assetIndex", "downloads", "logging"):
            self.assertEqual(descriptor[field], base[field])
        self.assertEqual(base["mainClass"], "net.minecraft.client.main.Main")
        self.assertEqual(len(base["libraries"]), 1)
        self.assertEqual(descriptor["javaVersion"]["majorVersion"], 8)


if __name__ == "__main__":
    unittest.main()
